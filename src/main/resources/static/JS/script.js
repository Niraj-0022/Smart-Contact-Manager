console.log("this is script file");

const toggleSidebar=() => {
	if($(".sidebar").is(":visible")){
		
		$(".sidebar").css("display","none");
		$(".content").css("margin-left","0%");
	}
	else{
		$(".sidebar").css("display","block");
		$(".content").css("margin-left","20%");
	}
}

const paymentStart = () => {

    console.log("Payment Started..");

    let amount = $("#payment_field").val();

    if (amount == "" || amount == null) {
        Swal.fire("Failed !!", "Amount is required !!", "error");
        return;
    }

    console.log("Amount = " + amount);

    $.ajax({
        url: "/user/create_order",
        type: "POST",

        data: JSON.stringify({
            amount: amount
        }),

        contentType: "application/json",
        dataType: "json",

        success: function(response) {

            console.log("Create Order Response:");
            console.log(response);

            if (response.status == "created") {

                let options = {

                    key: 'rzp_test_S7i6VnfDSHnVgo',

                    amount: response.amount,

                    currency: "INR",

                    name: "Smart Contact Manager",

                    description: "Donation",

                    image: "",

                    order_id: response.id,

                    handler: function(response) {

                        console.log("Payment ID:");
                        console.log(response.razorpay_payment_id);

                        console.log("Order ID:");
                        console.log(response.razorpay_order_id);

                        console.log("Signature:");
                        console.log(response.razorpay_signature);

                        console.log("Payment successful!!");

                        updatePaymentOnServer(
                            response.razorpay_payment_id,
                            response.razorpay_order_id,
                            "paid"
                        );
                    },

                    prefill: {
                        name: "",
                        email: "",
                        contact: ""
                    },

                    notes: {
                        address: "Software Developer"
                    },

                    theme: {
                        color: "#3399cc"
                    }
                };

                let rzp = new Razorpay(options);

                rzp.on("payment.failed", function(response) {

                    console.log(response.error.code);
                    console.log(response.error.description);
                    console.log(response.error.source);
                    console.log(response.error.step);
                    console.log(response.error.reason);
                    console.log(response.error.metadata.order_id);
                    console.log(response.error.metadata.payment_id);

                    swal(
                        "Failed !!",
                        "Oops payment failed !!",
                        "error"
                    );
                });

                rzp.open();

            } else {

                console.log("Order was not created");
            }
        },

        error: function(xhr) {

            console.log("AJAX ERROR");
            console.log(xhr.status);
            console.log(xhr.responseText);

            alert("Error: " + xhr.status);
        }
    });
};


function updatePaymentOnServer(payment_id, order_id, status) {

    $.ajax({

        url: "/user/update_order",

        data: JSON.stringify({
            payment_id: payment_id,
            order_id: order_id,
            status: status
        }),

        contentType: "application/json",

        type: "POST",

        dataType: "json",

        success: function(response) {

            console.log("Payment updated on server");
            console.log(response);

            swal(
                "Good job!",
                "Congrats!! Payment successful!!",
                "success"
            );
        },

        error: function(error) {

            console.log("Update payment error:");
            console.log(error);

            swal(
                "Failed !!",
                "Your payment was successful, but we did not get it on server",
                "error"
            );
        }
    });
}